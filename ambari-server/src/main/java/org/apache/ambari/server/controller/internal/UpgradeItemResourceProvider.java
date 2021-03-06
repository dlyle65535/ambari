/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradeItemEntity;
import org.apache.ambari.server.state.UpgradeHelper;

import com.google.inject.Inject;

/**
 * Manages the ability to get the status of upgrades.
 */
@StaticallyInject
public class UpgradeItemResourceProvider extends ReadOnlyResourceProvider {

  protected static final String UPGRADE_CLUSTER_NAME = "UpgradeItem/cluster_name";
  protected static final String UPGRADE_REQUEST_ID = "UpgradeItem/request_id";
  protected static final String UPGRADE_GROUP_ID = "UpgradeItem/group_id";
  protected static final String UPGRADE_ITEM_STAGE_ID = "UpgradeItem/stage_id";

  private static final Set<String> PK_PROPERTY_IDS = new HashSet<String>(
      Arrays.asList(UPGRADE_REQUEST_ID, UPGRADE_ITEM_STAGE_ID));
  private static final Set<String> PROPERTY_IDS = new HashSet<String>();

  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<Resource.Type, String>();
  private static Map<String, String> STAGE_MAPPED_IDS = new HashMap<String, String>();

  @Inject
  private static UpgradeDAO m_dao = null;


  static {
    // properties
    PROPERTY_IDS.add(UPGRADE_ITEM_STAGE_ID);
    PROPERTY_IDS.add(UPGRADE_GROUP_ID);
    PROPERTY_IDS.add(UPGRADE_REQUEST_ID);

    // !!! boo
    for (String p : StageResourceProvider.PROPERTY_IDS) {
      STAGE_MAPPED_IDS.put(p, p.replace("Stage/", "UpgradeItem/"));
    }
    PROPERTY_IDS.addAll(STAGE_MAPPED_IDS.values());

    // keys
    KEY_PROPERTY_IDS.put(Resource.Type.UpgradeItem, UPGRADE_ITEM_STAGE_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.UpgradeGroup, UPGRADE_GROUP_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Upgrade, UPGRADE_REQUEST_ID);
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, UPGRADE_CLUSTER_NAME);
  }

  /**
   * Constructor.
   *
   * @param controller  the controller
   */
  UpgradeItemResourceProvider(AmbariManagementController controller) {
    super(PROPERTY_IDS, KEY_PROPERTY_IDS, controller);
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    // the request should contain a single map of update properties...
    Iterator<Map<String,Object>> iterator = request.getProperties().iterator();
    if (iterator.hasNext()) {

      Map<String,Object> updateProperties = iterator.next();

      String statusPropertyId = STAGE_MAPPED_IDS.get(StageResourceProvider.STAGE_STATUS);
      String stageStatus      = (String) updateProperties.get(statusPropertyId);

      if (stageStatus != null) {

        HostRoleStatus desiredStatus = HostRoleStatus.valueOf(stageStatus);
        Set<Resource>  resources     = getResources(PropertyHelper.getReadRequest(), predicate);

        for (Resource resource : resources) {
          // Set the desired status on the underlying stage.
          Long requestId = (Long) resource.getPropertyValue(UPGRADE_REQUEST_ID);
          Long stageId   = (Long) resource.getPropertyValue(UPGRADE_ITEM_STAGE_ID);

          StageResourceProvider.updateStageStatus(requestId, stageId, desiredStatus);
        }
      }
    }
    notifyUpdate(Resource.Type.UpgradeItem, request, predicate);

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> results = new HashSet<Resource>();
    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String clusterName = (String) propertyMap.get(UPGRADE_CLUSTER_NAME);
      String requestIdStr = (String) propertyMap.get(UPGRADE_REQUEST_ID);
      String groupIdStr = (String) propertyMap.get(UPGRADE_GROUP_ID);
      String stageIdStr = (String) propertyMap.get(UPGRADE_ITEM_STAGE_ID);

      if (null == requestIdStr || requestIdStr.isEmpty()) {
        throw new IllegalArgumentException("The upgrade id is required when querying for upgrades");
      }

      if (null == groupIdStr || groupIdStr.isEmpty()) {
        throw new IllegalArgumentException("The upgrade group id is required when querying for upgrades");
      }

      Long requestId = Long.valueOf(requestIdStr);
      Long groupId = Long.valueOf(groupIdStr);
      Long stageId = null;
      if (null != stageIdStr) {
        stageId = Long.valueOf(stageIdStr);
      }

      List<UpgradeItemEntity> entities = new ArrayList<UpgradeItemEntity>();
      if (null == stageId) {
        UpgradeGroupEntity group = m_dao.findUpgradeGroup(groupId);

        if (null == group || null == group.getItems()) {
          throw new NoSuchResourceException(String.format("Cannot load upgrade for %s", requestIdStr));
        }

        entities = group.getItems();

      } else {
        UpgradeItemEntity entity = m_dao.findUpgradeItemByRequestAndStage(requestId, stageId);
        if (null != entity) {
          entities.add(entity);
        }
      }

      // !!! need to do some lookup for stages, so use a stageid -> resource for
      // when that happens
      Map<Long, Resource> resultMap = new HashMap<Long, Resource>();

      for (UpgradeItemEntity entity : entities) {
        Resource r = toResource(entity, requestPropertyIds);
        resultMap.put(entity.getStageId(), r);
      }

      if (!resultMap.isEmpty()) {
        if (null != clusterName) {
          UpgradeHelper helper = new UpgradeHelper();

          Set<Resource> stages = helper.getStageResources(clusterName, requestId,
              new ArrayList<Long>(resultMap.keySet()));

          for (Resource stage : stages) {
            Long l = (Long) stage.getPropertyValue(StageResourceProvider.STAGE_STAGE_ID);

            Resource r = resultMap.get(l);
            if (null != r) {
              for (String propertyId : StageResourceProvider.PROPERTY_IDS) {
                setResourceProperty(r, STAGE_MAPPED_IDS.get(propertyId),
                  stage.getPropertyValue(propertyId), requestPropertyIds);
              }
            }
          }
        }
        results.addAll(resultMap.values());
      }
    }
    return results;
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  private Resource toResource(UpgradeItemEntity item, Set<String> requestedIds) {
    ResourceImpl resource = new ResourceImpl(Resource.Type.UpgradeItem);

    UpgradeGroupEntity group = item.getGroupEntity();
    UpgradeEntity upgrade = group.getUpgradeEntity();

    setResourceProperty(resource, UPGRADE_REQUEST_ID, upgrade.getRequestId(), requestedIds);
    setResourceProperty(resource, UPGRADE_GROUP_ID, group.getId(), requestedIds);
    setResourceProperty(resource, UPGRADE_ITEM_STAGE_ID, item.getStageId(), requestedIds);

    return resource;
  }

}
