package com.example;

import org.springframework.data.repository.CrudRepository;

public interface OrderItemDomainStateRepository
    extends CrudRepository<OrderItemDomainState, OrderItemId> {}
