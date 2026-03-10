package com.hutech.demo.controller;

import com.hutech.demo.model.CategoryAttribute;
import com.hutech.demo.model.ParentCategory;
import com.hutech.demo.service.CategoryAttributeService;
import com.hutech.demo.service.ParentCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categories/parent")
@RequiredArgsConstructor
public class ParentCategoryController {

    private final ParentCategoryService parentCategoryService;
    private final CategoryAttributeService categoryAttributeService;

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        ParentCategory parent = parentCategoryService.getById(id);
        model.addAttribute("parentCategory", parent);
        model.addAttribute("attributes", categoryAttributeService.findByParentCategoryId(id));
        return "categories/edit-parent-category";
    }

    @PostMapping("/edit/{id}")
    public String updateParent(@PathVariable Long id,
                               @Valid @ModelAttribute("parentCategory") ParentCategory parentCategory,
                               BindingResult result,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (result.hasErrors()) {
            parentCategory.setId(id);
            model.addAttribute("parentCategory", parentCategory);
            model.addAttribute("attributes", categoryAttributeService.findByParentCategoryId(id));
            return "categories/edit-parent-category";
        }
        ParentCategory existing = parentCategoryService.getById(id);
        existing.setName(parentCategory.getName());
        existing.setSortOrder(parentCategory.getSortOrder());
        parentCategoryService.save(existing);
        redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật danh mục cha!");
        return "redirect:/categories";
    }

    @PostMapping("/edit/{id}/attributes")
    public String addAttribute(@PathVariable Long id,
                               @RequestParam String attributeName,
                               RedirectAttributes redirectAttributes) {
        if (attributeName == null || attributeName.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Tên đặc tính không được trống.");
            return "redirect:/categories/parent/edit/" + id;
        }
        ParentCategory parent = parentCategoryService.getById(id);
        CategoryAttribute attr = new CategoryAttribute();
        attr.setParentCategory(parent);
        attr.setName(attributeName.trim());
        attr.setSortOrder(parent.getAttributes() != null ? parent.getAttributes().size() + 1 : 1);
        categoryAttributeService.save(attr);
        redirectAttributes.addFlashAttribute("successMessage", "Đã thêm đặc tính: " + attributeName);
        return "redirect:/categories/parent/edit/" + id;
    }

    @PostMapping("/edit/{parentId}/attributes/delete/{attrId}")
    public String deleteAttribute(@PathVariable Long parentId,
                                  @PathVariable Long attrId,
                                  RedirectAttributes redirectAttributes) {
        categoryAttributeService.deleteById(attrId);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa đặc tính.");
        return "redirect:/categories/parent/edit/" + parentId;
    }
}
