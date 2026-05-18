---
name: WireMock
description: Implement Wiremock tests. Use when asking to implement a test with WireMock.
---

# WireMock

## Quick start

```java                                                                                                                                                                                                
  @RegisterExtension                                                                                                                                                                                       
  static WireMockExtension wm =                                                                                                                                                                            
      WireMockExtension.newInstance()                                                                                                                                                                      
          .options(                                                                                                                                                                                        
              WireMockConfiguration.wireMockConfig()                                                                                                                                                       
                  .dynamicPort()                                                                                                                                                                           
                  .usingFilesUnderClasspath("wiremock/bigquery"))                                                                                                                                          
          .build();                                                                                                                                                                                        
```                                                                                                                                                                                                           
Le client est initialisé avec NoCredentials, NetHttpTransport, et l'URL de base WireMock (wm.getRuntimeInfo().getHttpBaseUrl()).                                                                         

Les fichiers sont stockés dans les dossiers :
- Stubs (mappings) : `src/test/resources/wiremock/{technology}/mappings/{technology}-stubs.json`
- Réponses JSON : `src/test/resources/wiremock/{technology}/__files/`


Conventions de nommage des fichiers __files                                                                                                                                                              
- Erreurs : error-{type}.json (ex: error-not-found.json)
