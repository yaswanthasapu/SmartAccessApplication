package com.smartaccess.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;
import java.util.Map;
import java.util.List;

public class SecurityQuestionsPage extends BasePage {
    private static final String OTP_HEADING_TEXT = "Verify Mobile (******6458)";
    private static final By PROCEED = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='Proceed']");
    private static final By VALIDATION_FAILED = AppiumBy.xpath(
            "//android.view.View[@content-desc='Validation Failed\nValidation fail']");
    private static final By OTP_HEADING = AppiumBy.xpath(
            "//android.view.View[@content-desc='" + OTP_HEADING_TEXT + "']");
    private static final By OPEN = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='Open']");
    private static final By CANCEL = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='Cancel']");
    private static final By FAILED = AppiumBy.xpath(
            "//android.view.View[@content-desc='Failed']");

    private final WebDriverWait pageWait;

    public SecurityQuestionsPage(AndroidDriver driver) {
        super(driver);
        pageWait = new WebDriverWait(driver, Duration.ofSeconds(60));
    }

    public PostMcqOutcome answerSecurityQuestions(Map<String, String> answers) {
        Assert.assertNotNull(answers, "execution.securityAnswers is required.");
        pageWait.until(ignored -> answers.keySet().stream()
                .anyMatch(question -> firstDisplayed(questionLocator(question)) != null));
        int answered = 0;
        for (Map.Entry<String, String> answer : answers.entrySet())
            answered += selectSecurityAnswer(answer.getKey(), answer.getValue());
        Assert.assertEquals(answered, answers.size(),
                "Not all configured security questions could be answered.");
        captureStep("15_Security_Questions_Answered");
        scrollDownTo(PROCEED, "MCQ Proceed button");
        click(PROCEED);

        PostMcqOutcome outcome = pageWait.until(ignored -> {
            if (firstDisplayed(VALIDATION_FAILED) != null) return PostMcqOutcome.FAILED;
            if (new DiagnosticsPage(driver).isDisplayed()) return PostMcqOutcome.DIAGNOSTICS;
            if (firstDisplayed(OTP_HEADING) != null) return PostMcqOutcome.OTP;
            if (firstDisplayed(OPEN) != null || firstDisplayed(CANCEL) != null)
                return PostMcqOutcome.DOOR_CONFIRMATION;
            return null;
        });
        if (outcome == PostMcqOutcome.FAILED) {
            WebElement failure = firstDisplayed(VALIDATION_FAILED);
            captureStep("16_Security_Question_Validation_Failed");
            Assert.fail("Security-question validation failed: "
                    + (failure == null ? "Validation Failed" : failure.getAttribute("content-desc")));
        }
        return outcome;
    }

    private int selectSecurityAnswer(String questionText, String answer) {
        Assert.assertFalse(answer == null || answer.isBlank(),
                "An answer was not supplied for security question: " + questionText);
        scrollDownTo(questionLocator(questionText), "Security question: " + questionText);
        By answerLocator = AppiumBy.xpath(
                "//android.view.View[@content-desc=" + xpathLiteral(answer) + "]");
        scrollDownTo(answerLocator, "Answer for question: " + questionText);
        click(answerLocator);
        return 1;
    }

    private By questionLocator(String questionText) {
        return AppiumBy.xpath(
                "//android.view.View[contains(@content-desc, " + xpathLiteral(questionText) + ")]");
    }

    private void scrollDownTo(By locator, String label) {
        // Check first: do not scroll past a question that is already on screen.
        for (int attempt = 0; attempt <= 5; attempt++) {
            if (firstDisplayed(locator) != null) return;
            if (attempt == 5) break;
            System.out.println("[MCQ] Scrolling down to " + label
                    + " (" + (attempt + 1) + "/5)");
            swipeUp();
        }
        captureStep("MCQ_Target_Not_Found");
        Assert.fail(label + " was not visible after 5 downward scrolls.");
    }

    private void swipeUp() {
        // A finger swipe upward reveals lower content, including Flutter screens
        // that do not expose a native UiScrollable node.
        Dimension size = driver.manage().window().getSize();
        int x = size.width / 2;
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "mcq-scroll");
        Sequence swipe = new Sequence(finger, 0);
        swipe.addAction(finger.createPointerMove(Duration.ZERO,
                PointerInput.Origin.viewport(), x, (int) (size.height * 0.75)));
        swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(450),
                PointerInput.Origin.viewport(), x, (int) (size.height * 0.35)));
        swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(swipe));
    }

    private WebElement firstDisplayed(By locator) {
        for (WebElement element : driver.findElements(locator)) {
            try {
                if (element.isDisplayed()) return element;
            } catch (StaleElementReferenceException ignored) {
                // Flutter rebuilt the semantics tree between lookup and inspection.
            }
        }
        return null;
    }

    private static String xpathLiteral(String value) {
        if (!value.contains("'")) return "'" + value + "'";
        if (!value.contains("\"")) return "\"" + value + "\"";
        throw new IllegalArgumentException("Unsupported XPath value: " + value);
    }

    public enum PostMcqOutcome { OTP, DOOR_CONFIRMATION, DIAGNOSTICS, FAILED }
}
