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
package org.apache.ambari.server.orm.entities;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * The {@link AlertGroupEntity} class manages the logical groupings of
 * {@link AlertDefinitionEntity}s in order to easily define what alerts an
 * {@link AlertTargetEntity} should be notified on.
 */
@Entity
@Table(name = "alert_group", uniqueConstraints = @UniqueConstraint(columnNames = {
    "cluster_id", "group_name" }))
@NamedQueries({
    @NamedQuery(name = "AlertGroupEntity.findAll", query = "SELECT alertGroup FROM AlertGroupEntity alertGroup"),
    @NamedQuery(name = "AlertGroupEntity.findByName", query = "SELECT alertGroup FROM AlertGroupEntity alertGroup WHERE alertGroup.groupName = :groupName"), })
public class AlertGroupEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.TABLE)
  @Column(name = "group_id", unique = true, nullable = false, updatable = false)
  private Long groupId;

  @Column(name = "cluster_id", nullable = false)
  private Long clusterId;

  @Column(name = "group_name", nullable = false, length = 255)
  private String groupName;

  @Column(name = "is_default", nullable = false)
  private Integer isDefault = Integer.valueOf(0);

  /**
   * Bi-directional many-to-many association to {@link AlertDefinitionEntity}
   */
  @ManyToMany
  @JoinTable(name = "alert_grouping", joinColumns = { @JoinColumn(name = "group_id", nullable = false) }, inverseJoinColumns = { @JoinColumn(name = "definition_id", nullable = false) })
  private List<AlertDefinitionEntity> alertDefinitions;

  /**
   * Bi-directional many-to-many association to {@link AlertTargetEntity}
   */
  @ManyToMany(mappedBy = "alertGroups")
  private List<AlertTargetEntity> alertTargets;

  /**
   * Constructor.
   */
  public AlertGroupEntity() {
  }

  /**
   * Gets the unique ID of this grouping of alerts.
   * 
   * @return the ID (never {@code null}).
   */
  public Long getGroupId() {
    return groupId;
  }

  /**
   * Sets the unique ID of this grouping of alerts.
   * 
   * @param groupId
   *          the ID (not {@code null}).
   */
  public void setGroupId(Long groupId) {
    this.groupId = groupId;
  }

  /**
   * Gets the ID of the cluster that this alert group is a part of.
   * 
   * @return the ID (never {@code null}).
   */
  public Long getClusterId() {
    return clusterId;
  }

  /**
   * Sets the ID of the cluster that this alert group is a part of.
   * 
   * @param clusterId
   *          the ID of the cluster (not {@code null}).
   */
  public void setClusterId(Long clusterId) {
    this.clusterId = clusterId;
  }

  /**
   * Gets the name of the grouping of alerts. Group names are unique in a given
   * cluster.
   * 
   * @return the group name (never {@code null}).
   */
  public String getGroupName() {
    return groupName;
  }

  /**
   * Sets the name of this grouping of alerts. Group names are unique in a given
   * cluster.
   * 
   * @param groupName
   *          the name of the group (not {@code null}).
   */
  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  /**
   * Gets whether this is a default group and is mostly read-only. Default
   * groups cannot have their alert definition groupings changed. New alert
   * definitions are automtaically added to the default group that belongs to
   * the service of that definition.
   * 
   * @return {@code true} if this is a default group, {@code false} otherwise.
   */
  public boolean isDefault() {
    return isDefault == 0 ? false : true;
  }

  /**
   * Sets whether this is a default group and is immutable.
   * 
   * @param isDefault
   *          {@code true} to make this group immutable.
   */
  public void setDefault(boolean isDefault) {
    this.isDefault = isDefault == false ? 0 : 1;
  }

  /**
   * Gets all of the alert definitions that are a part of this grouping.
   * 
   * @return the alert definitions or {@code null} if none.
   */
  public List<AlertDefinitionEntity> getAlertDefinitions() {
    return alertDefinitions;
  }

  /**
   * Set all of the alert definitions that are part of this alert group.
   * 
   * @param alertDefinitions
   *          the definitions, or {@code null} for none.
   */
  public void setAlertDefinitions(List<AlertDefinitionEntity> alertDefinitions) {
    this.alertDefinitions = alertDefinitions;
  }

  /**
   * Gets all of the targets that will receive notifications for alert
   * definitions in this group.
   * 
   * @return the targets, or {@code null} if there are none.
   */
  public List<AlertTargetEntity> getAlertTargets() {
    return alertTargets;
  }

  /**
   * Sets all of the targets that will receive notifications for alert
   * definitions in this group.
   * 
   * @param alertTargets
   *          the targets, or {@code null} if there are none.
   */
  public void setAlertTargets(List<AlertTargetEntity> alertTargets) {
    this.alertTargets = alertTargets;
  }
}