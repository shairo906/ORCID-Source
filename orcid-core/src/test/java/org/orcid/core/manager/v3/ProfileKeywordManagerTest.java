package org.orcid.core.manager.v3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.orcid.core.BaseTest;
import org.orcid.core.manager.ClientDetailsEntityCacheManager;
import org.orcid.core.manager.ClientDetailsManager;
import org.orcid.core.manager.SourceNameCacheManager;
import org.orcid.core.manager.v3.read_only.RecordNameManagerReadOnly;
import org.orcid.jaxb.model.v3.release.common.Source;
import org.orcid.jaxb.model.v3.release.common.Visibility;
import org.orcid.jaxb.model.v3.release.record.Keyword;
import org.orcid.jaxb.model.v3.release.record.Keywords;
import org.orcid.persistence.dao.RecordNameDao;
import org.orcid.persistence.jpa.entities.ClientDetailsEntity;
import org.orcid.test.TargetProxyHelper;
import org.springframework.test.util.ReflectionTestUtils;

public class ProfileKeywordManagerTest extends BaseTest {
    private static final List<String> DATA_FILES = Arrays.asList("/data/SourceClientDetailsEntityData.xml",
            "/data/ProfileEntityData.xml", "/data/ClientDetailsEntityData.xml", "/data/RecordNameEntityData.xml");

    private static final String CLIENT_1_ID = "4444-4444-4444-4498";
    private String claimedOrcid = "0000-0000-0000-0002";
    private String unclaimedOrcid = "0000-0000-0000-0001";

    @Mock
    private SourceManager sourceManager;

    @Resource(name = "profileKeywordManagerV3")
    private ProfileKeywordManager profileKeywordManager;
    
    @Resource
    private ClientDetailsEntityCacheManager clientDetailsEntityCacheManager;
    
    @Resource
    private SourceNameCacheManager sourceNameCacheManager;
    
    @Resource
    private ClientDetailsManager clientDetailsManager;
    
    @Resource
    private RecordNameDao recordNameDao;
    
    @Resource(name = "recordNameManagerReadOnlyV3")
    private RecordNameManagerReadOnly recordNameManager;
    
    @Mock
    private ClientDetailsManager mockClientDetailsManager;
    
    @Mock
    private RecordNameDao mockRecordNameDao;
    
    @Mock
    private RecordNameManagerReadOnly mockRecordNameManager;


    @BeforeClass
    public static void initDBUnitData() throws Exception {
        initDBUnitData(DATA_FILES);
    }

    @Before
    public void before() {
        TargetProxyHelper.injectIntoProxy(profileKeywordManager, "sourceManager", sourceManager); 
        
        // by default return client details entity with user obo disabled
        Mockito.when(mockClientDetailsManager.findByClientId(Mockito.anyString())).thenReturn(new ClientDetailsEntity());
        ReflectionTestUtils.setField(clientDetailsEntityCacheManager, "clientDetailsManager", mockClientDetailsManager);
        
        Mockito.when(mockRecordNameDao.exists(Mockito.anyString())).thenReturn(true);
        Mockito.when(mockRecordNameManager.fetchDisplayablePublicName(Mockito.anyString())).thenReturn("test");
        ReflectionTestUtils.setField(sourceNameCacheManager, "recordNameDao", mockRecordNameDao);
        ReflectionTestUtils.setField(sourceNameCacheManager, "recordNameManagerReadOnlyV3", mockRecordNameManager);
    }

    @After
    public void tearDown() {
        ReflectionTestUtils.setField(clientDetailsEntityCacheManager, "clientDetailsManager", clientDetailsManager);        
        ReflectionTestUtils.setField(sourceNameCacheManager, "recordNameDao", recordNameDao);        
        ReflectionTestUtils.setField(sourceNameCacheManager, "recordNameManagerReadOnlyV3", recordNameManager);   
    }
    
    @AfterClass
    public static void removeDBUnitData() throws Exception {
        List<String> reversedDataFiles = new ArrayList<String>(DATA_FILES);
        Collections.reverse(reversedDataFiles);
        removeDBUnitData(reversedDataFiles);
    }

    @Test
    public void testAddKeywordToUnclaimedRecordPreserveKeywordVisibility() {
        when(sourceManager.retrieveActiveSource()).thenReturn(Source.forClient(CLIENT_1_ID));
        Keyword keyword = getKeyword();

        keyword = profileKeywordManager.createKeyword(unclaimedOrcid, keyword, true);
        keyword = profileKeywordManager.getKeyword(unclaimedOrcid, keyword.getPutCode());

        assertNotNull(keyword);
        assertEquals(Visibility.PUBLIC, keyword.getVisibility());
    }

    @Test
    public void testAddKeywordToClaimedRecordPreserveUserDefaultVisibility() {
        when(sourceManager.retrieveActiveSource()).thenReturn(Source.forClient(CLIENT_1_ID));
        Keyword keyword = getKeyword();

        keyword = profileKeywordManager.createKeyword(claimedOrcid, keyword, true);
        keyword = profileKeywordManager.getKeyword(claimedOrcid, keyword.getPutCode());

        assertNotNull(keyword);
        assertEquals(Visibility.LIMITED, keyword.getVisibility());
    }

    @Test
    public void displayIndexIsSetTo_1_FromUI() {
        when(sourceManager.retrieveActiveSource()).thenReturn(Source.forClient(CLIENT_1_ID));
        Keyword keyword = getKeyword();
        keyword.setContent(keyword.getContent() + " fromUI1");
        
        keyword = profileKeywordManager.createKeyword(claimedOrcid, keyword, false);
        keyword = profileKeywordManager.getKeyword(claimedOrcid, keyword.getPutCode());

        assertNotNull(keyword);
        assertEquals(Long.valueOf(1), keyword.getDisplayIndex());
    }
    
    @Test
    public void displayIndexIsSetTo_0_FromAPI() {
        when(sourceManager.retrieveActiveSource()).thenReturn(Source.forClient(CLIENT_1_ID));
        Keyword keyword = getKeyword();
        keyword.setContent(keyword.getContent() + " fromAPI1");        
        
        keyword = profileKeywordManager.createKeyword(claimedOrcid, keyword, true);
        keyword = profileKeywordManager.getKeyword(claimedOrcid, keyword.getPutCode());

        assertNotNull(keyword);
        assertEquals(Long.valueOf(0), keyword.getDisplayIndex());
    }
    
    @Test
    public void getAllTest() {
        String orcid = "0000-0000-0000-0003";
        Keywords elements = profileKeywordManager.getKeywords(orcid);
        assertNotNull(elements);
        assertNotNull(elements.getKeywords());
        assertEquals(5, elements.getKeywords().size());
        boolean found1 = false, found2 = false, found3 = false, found4 = false, found5 = false;
        for(Keyword element : elements.getKeywords()) {
            if(9 == element.getPutCode()){
                found1 = true;
            } else if(10 == element.getPutCode()){
                found2 = true;
            } else if(11 == element.getPutCode()){
                found3 = true;
            } else if(12 == element.getPutCode()){
                found4 = true;
            } else if(13 == element.getPutCode()){
                found5 = true;
            } else {
                fail("Invalid put code found: " + element.getPutCode());
            }
        }
        assertTrue(found1);
        assertTrue(found2);
        assertTrue(found3);
        assertTrue(found4);
        assertTrue(found5);
    }
    
    @Test
    public void getPublicTest() {
        String orcid = "0000-0000-0000-0003";        
        Keywords elements = profileKeywordManager.getPublicKeywords(orcid);
        assertNotNull(elements);
        assertNotNull(elements.getKeywords());
        assertEquals(1, elements.getKeywords().size());
        assertEquals(Long.valueOf(9), elements.getKeywords().get(0).getPutCode());
    }
    
    private Keyword getKeyword() {
        Keyword keyword = new Keyword();
        keyword.setContent("keyword-1");
        keyword.setVisibility(Visibility.PUBLIC);
        return keyword;
    }
}
