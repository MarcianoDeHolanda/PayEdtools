package ve.nottabaker.payedtools.utils;

import ve.nottabaker.payedtools.PayEdtools;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Utility for parsing and formatting currency amounts
 */
public class AmountParser {
    
    private final PayEdtools plugin;
    private final NumberFormat numberFormat;
    
    public AmountParser(PayEdtools plugin) {
        this.plugin = plugin;
        
        // Initialize number format based on config
        String localeStr = plugin.getConfigManager().getNumberFormatLocale();
        Locale locale;
        
        try {
            String[] parts = localeStr.split("_");
            if (parts.length == 2) {
                locale = new Locale(parts[0], parts[1]);
            } else {
                locale = Locale.US;
            }
        } catch (Exception e) {
            locale = Locale.US;
        }
        
        numberFormat = NumberFormat.getInstance(locale);
        int decimalPlaces = plugin.getConfigManager().getDecimalPlaces();
        numberFormat.setMaximumFractionDigits(decimalPlaces);
        numberFormat.setMinimumFractionDigits(0);
    }
    
    /**
     * Parse amount string with format support (k, M, B, T)
     */
    public double parse(String amountStr) throws NumberFormatException {
        if (amountStr == null || amountStr.isEmpty()) {
            throw new NumberFormatException("Amount cannot be empty");
        }
        
        amountStr = amountStr.trim();
        
        // Check if currency formats are enabled
        if (!plugin.getConfigManager().isCurrencyFormatsEnabled()) {
            return Double.parseDouble(amountStr);
        }
        
        // Check for format suffix
        Map<String, Double> formats = plugin.getConfigManager().getCurrencyFormats();
        
        for (Map.Entry<String, Double> entry : formats.entrySet()) {
            String suffix = entry.getKey();
            Double multiplier = entry.getValue();
            
            // Check both uppercase and lowercase
            if (amountStr.toLowerCase().endsWith(suffix.toLowerCase())) {
                String numberPart = amountStr.substring(0, amountStr.length() - suffix.length()).trim();
                
                try {
                    double number = Double.parseDouble(numberPart);
                    return number * multiplier;
                } catch (NumberFormatException e) {
                    throw new NumberFormatException("Invalid number format: " + numberPart);
                }
            }
        }
        
        // No format suffix found, parse as regular number
        return Double.parseDouble(amountStr);
    }
    
    /**
     * Format amount for display
     */
    public String format(double amount) {
        return numberFormat.format(amount);
    }
    
    /**
     * Format amount with currency name
     */
    public String formatWithCurrency(double amount, String currency) {
        String currencyName = plugin.getCurrencyManager().getCurrencyName(currency);
        return format(amount) + " " + currencyName;
    }
    
    /**
     * Validate amount against configured limits
     */
    public ValidationResult validate(double amount, boolean bypassLimits) {
        if (amount <= 0) {
            return new ValidationResult(false, "Amount must be positive");
        }
        
        if (!bypassLimits && plugin.getConfigManager().isLimitsEnabled()) {
            double min = plugin.getConfigManager().getMinimumAmount();
            double max = plugin.getConfigManager().getMaximumAmount();
            
            if (amount < min) {
                return new ValidationResult(false, "amount-too-low");
            }
            
            if (max > 0 && amount > max) {
                return new ValidationResult(false, "amount-too-high");
            }
        }
        
        return new ValidationResult(true, null);
    }
    
    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorKey;
        
        public ValidationResult(boolean valid, String errorKey) {
            this.valid = valid;
            this.errorKey = errorKey;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorKey() {
            return errorKey;
        }
    }
}
