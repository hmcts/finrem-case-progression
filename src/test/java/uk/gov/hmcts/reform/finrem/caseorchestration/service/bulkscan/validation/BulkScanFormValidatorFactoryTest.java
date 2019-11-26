package uk.gov.hmcts.reform.finrem.caseorchestration.service.bulkscan.validation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;

@RunWith(MockitoJUnitRunner.class)
public class BulkScanFormValidatorFactoryTest {

    @Rule
    public ExpectedException expectedException = none();

    @Mock
    private FormAValidator formAValidator;

    @InjectMocks
    private BulkScanFormValidatorFactory bulkScanFormValidatorFactory;

    @Test
    public void shouldReturnValidatorForFormA() {
        BulkScanFormValidator validator = bulkScanFormValidatorFactory.getValidator("formA");

        assertThat(validator, is(instanceOf(FormAValidator.class)));
    }

    @Test
    public void shouldThrowExceptionWhenFormTypeIsNotSupported() {
        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage("\"unsupportedFormType\" form type is not supported");

        bulkScanFormValidatorFactory.getValidator("unsupportedFormType");
    }
}