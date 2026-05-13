package com.livescore.backend.Controller;

import com.livescore.backend.DTO.AccountDTO;
import com.livescore.backend.Entity.Account;
import com.livescore.backend.Service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AccountController {

    @Autowired
    private AccountService accountService;

    @PostMapping("/account")
    public ResponseEntity<?> createAccount(@RequestBody Account account) {
        return accountService.createAccount(account);
    }

    @PostMapping("/account/login")
    public ResponseEntity<?> loginAccount(@RequestBody AccountDTO account) {
        return accountService.loginAccount(account);
    }

    @GetMapping("/account/{id}")
    public ResponseEntity<?> getAccountById(@PathVariable Long id) {

        return accountService.getAccountById(id);
    }

    @GetMapping("/account")
    public ResponseEntity<?> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    @PutMapping("/account/{id}")
    public ResponseEntity<?> updateAccount(@PathVariable Long id, @RequestBody Account account) {
        return accountService.updateAccount(id, account);
    }

    @DeleteMapping("/account/{id}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long id) {
        return accountService.deleteAccount(id);
    }

    @PutMapping("/account/{id}/restore")
    public ResponseEntity<?> restoreAccount(@PathVariable Long id) {
        return accountService.restoreAccount(id);
    }

    @GetMapping("/account/players/{tid}")
    public ResponseEntity<List<AccountDTO>> getAllPlayerAccounts(@PathVariable Long tid) {
        return accountService.getAllPlayerAccounts(tid);
    }



}
