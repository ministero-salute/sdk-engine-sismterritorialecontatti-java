package it.mds.sdk.flusso.sism.territoriale.cont.tracciato;

import it.mds.sdk.flusso.sism.territoriale.cont.parser.regole.RecordDtoSismTerritorialeContatti;
import it.mds.sdk.flusso.sism.territoriale.cont.parser.regole.conf.ConfigurazioneFlussoSismTerContatti;
import it.mds.sdk.flusso.sism.territoriale.cont.tracciato.bean.output.contatto.ObjectFactory;
import it.mds.sdk.flusso.sism.territoriale.cont.tracciato.bean.output.contatto.TerritorialeContatto;
import it.mds.sdk.gestorefile.GestoreFile;
import it.mds.sdk.gestorefile.factory.GestoreFileFactory;
import it.mds.sdk.libreriaregole.dtos.CampiInputBean;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
@MockitoSettings(strictness = Strictness.LENIENT)
class TracciatoSplitterImplTest {
    @InjectMocks
    @Spy
    private TracciatoSplitterImpl tracciatoSplitter;
    private ConfigurazioneFlussoSismTerContatti configurazione = Mockito.mock(ConfigurazioneFlussoSismTerContatti.class);
    private ObjectFactory objectFactory = Mockito.mock(ObjectFactory.class);
    private TerritorialeContatto territorialeContatto = Mockito.mock(TerritorialeContatto.class);
    private TerritorialeContatto.AziendaSanitariaRiferimento asl = Mockito.mock(TerritorialeContatto.AziendaSanitariaRiferimento.class);
    private ConfigurazioneFlussoSismTerContatti.XmlOutput xmlOutput = Mockito.mock(ConfigurazioneFlussoSismTerContatti.XmlOutput.class);
    private MockedStatic<GestoreFileFactory> gestore;
    private GestoreFile gestoreFile = Mockito.mock(GestoreFile.class);
    private TerritorialeContatto.AziendaSanitariaRiferimento aziendaSanitariaRiferimento = Mockito.mock(TerritorialeContatto.AziendaSanitariaRiferimento.class);
    private List<TerritorialeContatto.AziendaSanitariaRiferimento> aziendaSanitariaRiferimentoList = new ArrayList<>();
    private TerritorialeContatto.AziendaSanitariaRiferimento.DSM dsm = Mockito.mock(TerritorialeContatto.AziendaSanitariaRiferimento.DSM.class);
    private List<TerritorialeContatto.AziendaSanitariaRiferimento.DSM> listDsm = new ArrayList<>();
    private TerritorialeContatto.AziendaSanitariaRiferimento.DSM.Assistito assistito = Mockito.mock(TerritorialeContatto.AziendaSanitariaRiferimento.DSM.Assistito.class);
    private RecordDtoSismTerritorialeContatti r = new RecordDtoSismTerritorialeContatti();
    List<RecordDtoSismTerritorialeContatti> records = new ArrayList<>();

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        gestore = mockStatic(GestoreFileFactory.class);
        initMockedRecord(r);
        records.add(r);
    }

    @Test
    void dividiTracciatoTest() throws JAXBException, IOException, SAXException {

        when(tracciatoSplitter.getConfigurazione()).thenReturn(configurazione);
        when(objectFactory.createTerritorialeContatto()).thenReturn(territorialeContatto);
        when(territorialeContatto.getAziendaSanitariaRiferimento()).thenReturn(List.of(asl));
        when(configurazione.getXmlOutput()).thenReturn(xmlOutput);
        when(xmlOutput.getPercorso()).thenReturn("percorso");
        gestore.when(() -> GestoreFileFactory.getGestoreFile("XML")).thenReturn(gestoreFile);
        doNothing().when(gestoreFile).scriviDto(any(), any(), any());

        Assertions.assertEquals(
                List.of(Path.of("percorsoSDK_TER_CNT_S1_100.xml")),
                this.tracciatoSplitter.dividiTracciato(records, "100")
        );

    }

    @Test
    void dividiTracciatoTestOk2() throws JAXBException, IOException, SAXException {
        when(tracciatoSplitter.getConfigurazione()).thenReturn(configurazione);
        when(objectFactory.createTerritorialeContatto()).thenReturn(territorialeContatto);
        when(territorialeContatto.getAziendaSanitariaRiferimento()).thenReturn(List.of(asl));

        when(configurazione.getXmlOutput()).thenReturn(xmlOutput);
        when(xmlOutput.getPercorso()).thenReturn("percorso");
        gestore.when(() -> GestoreFileFactory.getGestoreFile("XML")).thenReturn(gestoreFile);
        doNothing().when(gestoreFile).scriviDto(any(), any(), any());

        doReturn(null).when(tracciatoSplitter).getCurrentAsl(any(), any());
        doReturn(null).when(tracciatoSplitter).getCurrentDSM(any(), any());
        Assertions.assertEquals(
                List.of(Path.of("percorsoSDK_TER_CNT_S1_100.xml")),
                this.tracciatoSplitter.dividiTracciato(records, "100")
        );

    }

    @Test
    void getCurrentDsmTest() {
        var list = List.of(dsm);
        when(asl.getDSM()).thenReturn(list);
        var c = tracciatoSplitter.getCurrentDSM(asl, r);
    }

    @Test
    void getCurrentAslTest() {
        var list = List.of(asl);

        when(territorialeContatto.getAziendaSanitariaRiferimento()).thenReturn(list);
        var c = tracciatoSplitter.getCurrentAsl(territorialeContatto, r);
    }

    @Test
    void creaPrestazioniSanitarieTest() {
        var list = List.of(asl);
        var c = tracciatoSplitter.creaTerritorialeContatti(records, null);
    }

    @AfterEach
    void closeMocks() {
        gestore.close();
    }

    private void initMockedRecord(RecordDtoSismTerritorialeContatti r) {
        CampiInputBean campiInputBean = new CampiInputBean();
        campiInputBean.setPeriodoRiferimentoInput("Q1");
        campiInputBean.setAnnoRiferimentoInput("2022");
        r.setTipoOperazioneContatto("C");
        r.setAnnoRiferimento("2022");
        r.setCodiceRegione("080");
        r.setPeriodoRiferimento("S1");
        r.setCodiceDipartimentoSaluteMentale("cdsm");
        r.setCodiceAziendaSanitariaRiferimento("casr");
        r.setIdContatto(1L);
        r.setIdRecord("ic");
        r.setCodiceStruttura("abc");
        records.add(r);
    }

}