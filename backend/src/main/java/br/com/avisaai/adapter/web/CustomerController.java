package br.com.avisaai.adapter.web;

import br.com.avisaai.adapter.web.Dtos.CustomerIn;
import br.com.avisaai.adapter.web.Dtos.CustomerOut;
import br.com.avisaai.adapter.web.Dtos.ReminderOut;
import br.com.avisaai.application.NotFoundException;
import br.com.avisaai.application.Repositories.CustomerRepository;
import br.com.avisaai.application.Repositories.ReminderRepository;
import br.com.avisaai.domain.Customer;
import br.com.avisaai.domain.Phone;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Clientes", description = "Quem recebe os avisos (com consentimento LGPD)")
@Transactional
public class CustomerController {

    private final CustomerRepository customers;
    private final ReminderRepository reminders;

    public CustomerController(CustomerRepository customers, ReminderRepository reminders) {
        this.customers = customers;
        this.reminders = reminders;
    }

    @GetMapping
    @Operation(summary = "Lista clientes, com busca opcional por nome ou telefone")
    public List<CustomerOut> list(@RequestParam(required = false) String q) {
        List<Customer> found = q == null || q.isBlank()
                ? customers.findAllByOrderByNameAsc()
                : customers.findByNameContainingIgnoreCaseOrPhoneContainingOrderByNameAsc(q.trim(), q.replaceAll("\\D", "").isEmpty() ? "\u0000" : q.replaceAll("\\D", ""));
        return found.stream().map(CustomerOut::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra cliente (telefone brasileiro com DDD; o DDI 55 é acrescentado)")
    public CustomerOut create(@Valid @RequestBody CustomerIn in) {
        Customer c = new Customer(in.name().trim(), in.phone(), in.consent());
        c.setNotes(in.notes());
        return CustomerOut.of(customers.save(c));
    }

    @PutMapping("/{id}")
    public CustomerOut update(@PathVariable Long id, @Valid @RequestBody CustomerIn in) {
        Customer c = get(id);
        c.setName(in.name().trim());
        c.setPhone(Phone.normalize(in.phone()));
        c.setConsent(in.consent());
        c.setNotes(in.notes());
        return CustomerOut.of(c);
    }

    @GetMapping("/{id}/reminders")
    @Operation(summary = "Histórico de compromissos do cliente")
    public List<ReminderOut> history(@PathVariable Long id) {
        get(id);
        return reminders.findByCustomerIdOrderByEventAtDesc(id).stream().map(ReminderOut::of).toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove o cliente (só sem compromissos; LGPD: direito de exclusão)")
    public void delete(@PathVariable Long id) {
        if (!reminders.findByCustomerIdOrderByEventAtDesc(id).isEmpty()) {
            throw new IllegalStateException("Cliente tem compromissos: exclua-os antes");
        }
        customers.delete(get(id));
    }

    private Customer get(Long id) {
        return customers.findById(id).orElseThrow(() -> new NotFoundException("Cliente", id));
    }
}
