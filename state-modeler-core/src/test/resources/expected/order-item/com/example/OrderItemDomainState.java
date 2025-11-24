package com.example;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

@Table("order_item_state")
public record OrderItemDomainState(
    @Id @Column("order_item_id") OrderItemId entityId,

    @Column("state_type") String stateType,

    @Column("state_row_id") Long stateRowId,

    @Column("state_at") Instant stateAt,

    @Column("state_json") String stateJson) {}
