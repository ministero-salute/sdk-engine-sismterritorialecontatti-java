package it.mds.sdk.flusso.sism.territoriale.cont.tracciato;

import it.mds.sdk.flusso.sism.territoriale.cont.parser.regole.RecordDtoSismTerritorialeContatti;
import it.mds.sdk.flusso.sism.territoriale.cont.parser.regole.conf.ConfigurazioneFlussoSismTerContatti;
import it.mds.sdk.flusso.sism.territoriale.cont.tracciato.bean.output.contatto.ObjectFactory;
import it.mds.sdk.flusso.sism.territoriale.cont.tracciato.bean.output.contatto.PeriodoRiferimento;
import it.mds.sdk.flusso.sism.territoriale.cont.tracciato.bean.output.contatto.TerritorialeContatto;
import it.mds.sdk.gestorefile.GestoreFile;
import it.mds.sdk.gestorefile.factory.GestoreFileFactory;
import it.mds.sdk.libreriaregole.tracciato.TracciatoSplitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component("tracciatoSplitterSismTerCont")
public class TracciatoSplitterImpl implements TracciatoSplitter<RecordDtoSismTerritorialeContatti> {

    @Override
    public List<Path> dividiTracciato(Path tracciato) {
        return null;
    }

    @Override
    public List<Path> dividiTracciato(List<RecordDtoSismTerritorialeContatti> records, String idRun) {

        try {
            ConfigurazioneFlussoSismTerContatti conf = getConfigurazione();
            String annoRif = records.get(0).getAnnoRiferimento();
            String codiceRegione = records.get(0).getCodiceRegione();

            //XML CONTATTO
            ObjectFactory objContatto = getObjectFactory();
            TerritorialeContatto territorialeContatto = objContatto.createTerritorialeContatto();
            territorialeContatto.setCodiceRegione(codiceRegione);
            territorialeContatto.setAnnoRiferimento(annoRif);
            territorialeContatto.setPeriodoRiferimento(it.mds.sdk.flusso.sism.territoriale.cont.tracciato.bean.output.contatto.PeriodoRiferimento.fromValue(records.get(0).getPeriodoRiferimento()));

            for (RecordDtoSismTerritorialeContatti r : records) {
                if (!r.getTipoOperazioneContatto().equalsIgnoreCase("NM")) {
                    creaContattoXml(r, territorialeContatto, objContatto);
                }
            }

            //recupero il path del file xsd di contatti
            URL resourceContatti = this.getClass().getClassLoader().getResource("CNT.xsd");
            log.debug("URL dell'XSD per la validazione idrun {} : {}", idRun, resourceContatti);

            //scrivi XML CONTATTO
            String pathContatto = conf.getXmlOutput().getPercorso() + "SDK_TER_CNT_" + records.get(0).getPeriodoRiferimento() + "_" + idRun + ".xml";


            return List.of(Path.of(pathContatto));
        } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
            log.error("[{}].dividiTracciato  - records[{}]  - idRun[{}] -" + e.getMessage(),
                    this.getClass().getName(),
                    e
            );
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossibile validare il csv in ingresso. message: " + e.getMessage());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void creaContattoXml(RecordDtoSismTerritorialeContatti r, TerritorialeContatto territorialeContatto,
                                 ObjectFactory objContatto) {


        //ASL RIF
        TerritorialeContatto.AziendaSanitariaRiferimento currentAsl = getCurrentAsl(territorialeContatto, r);
        if (currentAsl == null) {
            currentAsl = creaAslContatto(r.getCodiceAziendaSanitariaRiferimento(), objContatto);
            territorialeContatto.getAziendaSanitariaRiferimento().add(currentAsl);

        }

        //DSM
        TerritorialeContatto.AziendaSanitariaRiferimento.DSM currentDsm = getCurrentDSM(currentAsl, r);
        if (currentDsm == null) {
            currentDsm = creaDSMContatto(r.getCodiceDipartimentoSaluteMentale(), objContatto);
            currentAsl.getDSM().add(currentDsm);
        }

        //ASSISTITO
        TerritorialeContatto.AziendaSanitariaRiferimento.DSM.Assistito currentAssisitito = getCurrentAssistito(currentDsm, r);
        if (currentAssisitito == null) {
            currentAssisitito = creaAssistitoContatto(r, objContatto);
            currentDsm.getAssistito().add(currentAssisitito);
        }

        //STRUTTURA
        TerritorialeContatto.AziendaSanitariaRiferimento.DSM.Assistito.Struttura currentStruttura = getCurrentStruttura(currentAssisitito, r);
        if (currentStruttura == null) {
            currentStruttura = creaStrutturaContatto(r, objContatto);
            currentAssisitito.getStruttura().add(currentStruttura);
        }

        //CONTATTO

        TerritorialeContatto.AziendaSanitariaRiferimento.DSM.Assistito.Struttura.Contatto currentContatto = creaContatto(r, objContatto);
        currentStruttura.getContatto().add(currentContatto);
    }

    private TerritorialeContatto.AziendaSanitariaRiferimento creaAslContatto(String codAsl,
                                                                             ObjectFactory objContatto) {
        TerritorialeContatto.AziendaSanitariaRiferimento asl = objContatto.createTerritorialeContattoAziendaSanitariaRiferimento();
        asl.setCodiceAziendaSanitariaRiferimento(codAsl);
        return asl;
    }

    private TerritorialeContatto.AziendaSanitariaRiferimento.DSM creaDSMContatto(String codDsm,
                                                                                 ObjectFactory objContatto) {
        TerritorialeContatto.AziendaSanitariaRiferimento.DSM dsm = objContatto.createTerritorialeContattoAziendaSanitariaRiferimentoDSM();
        dsm.setCodiceDSM(codDsm);
        return dsm;
    }

    private TerritorialeContatto.AziendaSanitariaRiferimento.DSM.Assistito creaAssistitoContatto(RecordDtoSismTerritorialeContatti r,
                                                                                                 ObjectFactory objContatto) {
        TerritorialeContatto.AziendaSanitariaRiferimento.DSM.Assistito assistito = objContatto.createTerritorialeContattoAziendaSanitariaRiferimentoDSMAssistito();
        assistito.setIdRec(r.getIdRecord());
        return assistito;
    }

    private TerritorialeContatto.AziendaSanitariaRiferimento.DSM.Assistito.Struttura creaStrutturaContatto(RecordDtoSismTerritorialeContatti r,
                                                                                                           ObjectFactory objContatto) {
        TerritorialeContatto.AziendaSanitariaRiferimento.DSM.Assistito.Struttura struttura = objContatto.createTerritorialeContattoAziendaSanitariaRiferimentoDSMAssistitoStruttura();
        struttura.setCodiceStruttura(r.getCodiceStruttura());
        return struttura;
    }

    private TerritorialeContatto.AziendaSanitariaRiferimento.DSM.Assistito.Struttura.Contatto creaContatto(RecordDtoSismTerritorialeContatti r,
                                                                                                           ObjectFactory objContatto) {
        TerritorialeContatto.AziendaSanitariaRiferimento.DSM.Assistito.Struttura.Contatto contatto = objContatto.createTerritorialeContattoAziendaSanitariaRiferimentoDSMAssistitoStrutturaContatto();
        contatto.setIDContatto(r.getIdContatto());
        contatto.setInviantePrimoContatto(r.getInviantePrimoContatto());
        XMLGregorianCalendar dataAp = null;
        XMLGregorianCalendar dataChiusura = null;
        try {
            dataAp = r.getDataAperturaSchedaPaziente() != null ? DatatypeFactory.newInstance().newXMLGregorianCalendar(r.getDataAperturaSchedaPaziente()) : null;
            dataChiusura = r.getDataChiusuraSchedaPaziente() != null ? DatatypeFactory.newInstance().newXMLGregorianCalendar(r.getDataChiusuraSchedaPaziente()) : null;
        } catch (DatatypeConfigurationException e) {
            log.error("Errore conversione XMLGregorianCalendar date", e);
        }
        contatto.setDataAperturaSchedaPaziente(dataAp);
        contatto.setTipoOperazione(it.mds.sdk.flusso.sism.territoriale.cont.tracciato.bean.output.contatto.TipoOperazione.fromValue(r.getTipoOperazioneContatto()));
        contatto.setPrecedentiContatti(r.getPrecedentiContatti());
        contatto.setDataChiusuraSchedaPaziente(dataChiusura);
        contatto.setDiagnosiApertura(r.getDiagnosiApertura());
        contatto.setDiagnosiChiusura(r.getDiagnosiChiusura());
        contatto.setModalitaConclusione(r.getModalitàConclusione());
        return contatto;
    }

    public TerritorialeContatto creaTerritorialeContatti(List<RecordDtoSismTerritorialeContatti> records, TerritorialeContatto territorialeContatto) {

        //Imposto gli attribute element
        String annoRif = records.get(0).getAnnoRiferimento();
        String codiceRegione = records.get(0).getCodiceRegione();

        if (territorialeContatto == null) {
            ObjectFactory objContatto = getObjectFactory();
            territorialeContatto = objContatto.createTerritorialeContatto();
            territorialeContatto.setCodiceRegione(codiceRegione);
            territorialeContatto.setAnnoRiferimento(annoRif);
            territorialeContatto.setPeriodoRiferimento(PeriodoRiferimento.fromValue(records.get(0).getPeriodoRiferimento()));


            for (RecordDtoSismTerritorialeContatti r : records) {
                if (!r.getTipoOperazioneContatto().equalsIgnoreCase("NM")) {
                    creaContattoXml(r, territorialeContatto, objContatto);
                }
            }

        }
        return territorialeContatto;
    }

    public TerritorialeContatto.AziendaSanitariaRiferimento getCurrentAsl(TerritorialeContatto territorialeContatto, RecordDtoSismTerritorialeContatti r) {
        return territorialeContatto.getAziendaSanitariaRiferimento()
                .stream()
                .filter(asl -> r.getCodiceAziendaSanitariaRiferimento().equalsIgnoreCase(asl.getCodiceAziendaSanitariaRiferimento()))
                .findFirst()
                .orElse(null);
    }

    public TerritorialeContatto.AziendaSanitariaRiferimento.DSM getCurrentDSM(TerritorialeContatto.AziendaSanitariaRiferimento currentAsl, RecordDtoSismTerritorialeContatti r) {
        return currentAsl.getDSM()
                .stream()
                .filter(dsm -> r.getCodiceDipartimentoSaluteMentale().equalsIgnoreCase(dsm.getCodiceDSM()))
                .findFirst()
                .orElse(null);
    }

    private TerritorialeContatto.AziendaSanitariaRiferimento.DSM.Assistito getCurrentAssistito(TerritorialeContatto.AziendaSanitariaRiferimento.DSM currentDsm, RecordDtoSismTerritorialeContatti r) {
        return currentDsm.getAssistito()
                .stream()
                .filter(ass -> r.getIdRecord().equalsIgnoreCase(ass.getIdRec()))
                .findFirst()
                .orElse(null);
    }

    public TerritorialeContatto.AziendaSanitariaRiferimento.DSM.Assistito.Struttura getCurrentStruttura(TerritorialeContatto.AziendaSanitariaRiferimento.DSM.Assistito currentAssisitito, RecordDtoSismTerritorialeContatti r) {
        return currentAssisitito.getStruttura()
                .stream()
                .filter(str -> r.getCodiceStruttura().equalsIgnoreCase(str.getCodiceStruttura()))
                .findFirst()
                .orElse(null);
    }

    public ConfigurazioneFlussoSismTerContatti getConfigurazione() {
        return new ConfigurazioneFlussoSismTerContatti();
    }

    private ObjectFactory getObjectFactory() {
        return new ObjectFactory();
    }
}
