package com.petway.favorite.controller;
import com.petway.favorite.dto.*;
import com.petway.favorite.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/favorites") @RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService service;
    @GetMapping public List<FavoriteResponse> list(Authentication a) { return service.list(a.getName()); }
    @PostMapping public ResponseEntity<FavoriteResponse> toggle(Authentication a, @RequestBody FavoriteRequest r) { var result = service.toggle(a.getName(), r); return result == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(result); }
    @DeleteMapping("/{type}/{id}") public ResponseEntity<Void> delete(Authentication a, @PathVariable String type, @PathVariable String id) { service.delete(a.getName(), type, id); return ResponseEntity.noContent().build(); }
}
