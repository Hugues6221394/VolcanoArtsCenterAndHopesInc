package com.volcanoartscenter.platform.web.external.talentapplicant.controller;

import com.volcanoartscenter.platform.shared.model.TalentApplication;
import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.shared.service.CaptchaService;
import com.volcanoartscenter.platform.web.external.talentapplicant.service.TalentApplicantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class TalentApplicantController {

    private final TalentApplicantService talentApplicantService;
    private final CaptchaService captchaService;

    @GetMapping("/talent/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User user = currentUser(authentication).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("currentPage", "talent");
        model.addAttribute("pageTitle", "Talent Applicant Dashboard");
        model.addAttribute("user", user);
        model.addAttribute("applications", talentApplicantService.applicationsFor(user));
        model.addAttribute("latestApplication", talentApplicantService.latestFor(user).orElse(null));
        model.addAttribute("categories", TalentApplication.ApplicantCategory.values());
        model.addAttribute("areas", TalentApplication.TalentArea.values());
        return "external/talent-applicant/dashboard";
    }

    @GetMapping("/talent/register")
    public String registerPage(Model model) {
        model.addAttribute("currentPage", "talent");
        model.addAttribute("pageTitle", "Talent Applicant Registration");
        return "external/talent-applicant/register";
    }

    @PostMapping("/talent/register")
    public String register(@RequestParam String firstName,
                           @RequestParam String lastName,
                           @RequestParam String email,
                           @RequestParam(required = false) String phone,
                           @RequestParam(required = false) String country,
                           @RequestParam String password,
                           RedirectAttributes redirectAttributes) {
        try {
            talentApplicantService.registerApplicantAccount(firstName, lastName, email, phone, country, password);
            redirectAttributes.addFlashAttribute("successMessage", "Talent applicant account created. Please sign in.");
            return "redirect:/login";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("successMessage", ex.getMessage());
            return "redirect:/talent/register";
        }
    }

    @PostMapping("/talent/dashboard/apply")
    public String apply(Authentication authentication,
                        @RequestParam String fullName,
                        @RequestParam(required = false) String email,
                        @RequestParam(required = false) String phone,
                        @RequestParam(required = false) String ageRange,
                        @RequestParam(required = false) String gender,
                        @RequestParam(required = false) String location,
                        @RequestParam TalentApplication.ApplicantCategory applicantCategory,
                        @RequestParam TalentApplication.TalentArea talentArea,
                        @RequestParam(required = false) String experienceDescription,
                        @RequestParam(required = false) String motivation,
                        @RequestParam(required = false) String availabilityDetails,
                        @RequestParam(required = false) String accessibilityNeeds,
                        @RequestParam(defaultValue = "EMAIL") String preferredContactChannel,
                        @RequestParam(required = false) String captchaToken,
                        RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        if (!captchaService.verify(captchaToken)) {
            redirectAttributes.addFlashAttribute("successMessage", "Captcha validation failed.");
            return "redirect:/talent/dashboard";
        }
        talentApplicantService.createApplication(user, fullName, email, phone, ageRange, gender, location, applicantCategory, talentArea,
                experienceDescription, motivation, availabilityDetails, accessibilityNeeds, preferredContactChannel);
        redirectAttributes.addFlashAttribute("successMessage", "Application submitted successfully.");
        return "redirect:/talent/dashboard";
    }

    @PostMapping("/talent/dashboard/applications/{id}/update")
    public String updateApplication(Authentication authentication,
                                    @PathVariable Long id,
                                    @RequestParam(required = false) String ageRange,
                                    @RequestParam(required = false) String gender,
                                    @RequestParam(required = false) String location,
                                    @RequestParam(required = false) String experienceDescription,
                                    @RequestParam(required = false) String motivation,
                                    @RequestParam(required = false) String availabilityDetails,
                                    @RequestParam(required = false) String accessibilityNeeds,
                                    @RequestParam(defaultValue = "EMAIL") String preferredContactChannel,
                                    RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            talentApplicantService.updateOwnApplication(user, id, ageRange, gender, location, experienceDescription, motivation,
                    availabilityDetails, accessibilityNeeds, preferredContactChannel);
            redirectAttributes.addFlashAttribute("successMessage", "Application updated.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("successMessage", ex.getMessage());
        }
        return "redirect:/talent/dashboard";
    }

    @PostMapping("/talent/dashboard/profile")
    public String updateProfile(Authentication authentication,
                                @RequestParam String firstName,
                                @RequestParam String lastName,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String country,
                                RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setCountry(country);
        talentApplicantService.saveProfile(user);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated.");
        return "redirect:/talent/dashboard";
    }

    private Optional<User> currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            return Optional.empty();
        }
        return talentApplicantService.findUserByEmail(authentication.getName());
    }
}
