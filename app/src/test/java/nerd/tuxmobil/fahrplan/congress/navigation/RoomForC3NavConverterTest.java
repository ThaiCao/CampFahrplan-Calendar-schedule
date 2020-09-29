package nerd.tuxmobil.fahrplan.congress.navigation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnit4.class)
public class RoomForC3NavConverterTest {

    @Test
    public void convertWithAW1120() {
        assertThat(RoomForC3NavConverter.convert("AW1.120")).isEqualTo("aw1120");
    }

    @Test
    public void convertWithH1301Cornil() {
        assertThat(RoomForC3NavConverter.convert("H.1301 (Cornil)")).isEqualTo("h1301_cornil");
    }

    @Test
    public void convertWithJanson() {
        assertThat(RoomForC3NavConverter.convert("Janson")).isEqualTo("janson");
    }

    @Test
    public void convertWithUD2Corridor() {
        assertThat(RoomForC3NavConverter.convert("UD2.Corridor")).isEqualTo("ud2corridor");
    }

    @Test
    public void convertWithNonExisting() {
        assertThat(RoomForC3NavConverter.convert("NonExisting")).isEmpty();
    }

    @Test
    public void convertWithNull() {
        assertThat(RoomForC3NavConverter.convert(null)).isEmpty();
    }

}
