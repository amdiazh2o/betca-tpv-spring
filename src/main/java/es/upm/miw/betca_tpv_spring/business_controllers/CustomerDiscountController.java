package es.upm.miw.betca_tpv_spring.business_controllers;

import es.upm.miw.betca_tpv_spring.documents.CustomerDiscount;
import es.upm.miw.betca_tpv_spring.documents.User;
import es.upm.miw.betca_tpv_spring.dtos.CustomerDiscountDto;
import es.upm.miw.betca_tpv_spring.exceptions.NotFoundException;
import es.upm.miw.betca_tpv_spring.repositories.CustomerDiscountReactRepository;
import es.upm.miw.betca_tpv_spring.repositories.CustomerDiscountRepository;
import es.upm.miw.betca_tpv_spring.repositories.UserReactRepository;
import es.upm.miw.betca_tpv_spring.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
public class CustomerDiscountController {

    private static final String USER_NOT_FOUND = "User not found";
    private CustomerDiscountReactRepository customerDiscountReactRepository;
    private UserReactRepository userReactRepository;

    @Autowired
    CustomerDiscountController(CustomerDiscountReactRepository customerDiscountReactRepository, UserReactRepository userReactRepository,
                               CustomerDiscountRepository customerDiscountRepository, UserRepository userRepository) {
        this.customerDiscountReactRepository = customerDiscountReactRepository;
        this.userReactRepository = userReactRepository;
    }

    public Mono<User> findUserByMobile(String mobile) {
        return this.userReactRepository.findByMobile(mobile);
    }

    public Mono<CustomerDiscountDto> findByUserMobile(String mobile) {
        Mono<User> user = this.findUserByMobile(mobile);
        return this.customerDiscountReactRepository.findByUser(user)
                .switchIfEmpty(Mono.error(new NotFoundException(USER_NOT_FOUND)))
                .map(CustomerDiscountDto::new);
    }

    public Mono<CustomerDiscountDto> createCustomerDiscount(CustomerDiscountDto customerDiscountDto) {
        CustomerDiscount customerDiscount = new CustomerDiscount();
        return this.findUserByMobile(customerDiscountDto.getUser().getMobile()).doOnNext(user -> {
            customerDiscount.setDiscount(customerDiscountDto.getDiscount());
            customerDiscount.setUser(user);
        })
                .then(this.customerDiscountReactRepository.save(customerDiscount)).map(CustomerDiscountDto::new);

    }

    public Mono<CustomerDiscountDto> updateCustomerDiscount(String customerDiscountId, CustomerDiscountDto customerDiscountDto) {
        Mono<CustomerDiscount> customerDiscountMono = this.customerDiscountReactRepository.findById(customerDiscountId)
                .switchIfEmpty(Mono.error(new NotFoundException("CustomerDiscount Id " + customerDiscountId)))
                .map(customerDiscount1 -> {
                    customerDiscount1.setDescription(customerDiscountDto.getDescription());
                    customerDiscount1.setDiscount(customerDiscountDto.getDiscount());
                    customerDiscount1.setMinimumPurchase(customerDiscountDto.getMinimumPurchase());
                    return customerDiscount1;
                });
        return Mono
                .when(customerDiscountMono)
                .then(this.customerDiscountReactRepository.saveAll(customerDiscountMono).next().map(CustomerDiscountDto::new));

    }

    public Mono<Void> deleteCustomerDiscount(String customerDiscountId) {
        Mono<CustomerDiscount> customerDiscountMono = this.customerDiscountReactRepository.findById(customerDiscountId);
        return Mono
                .when(customerDiscountMono)
                .then(this.customerDiscountReactRepository.deleteById(customerDiscountId));
    }

    public Flux<CustomerDiscountDto> readAll() {
        return this.customerDiscountReactRepository.findAll().map(CustomerDiscountDto::new);
    }

}