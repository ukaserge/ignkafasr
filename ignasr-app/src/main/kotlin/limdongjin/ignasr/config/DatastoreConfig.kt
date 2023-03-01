package limdongjin.ignasr.config

//import org.springframework.cloud.gcp.data.datastore.repository.config.EnableDatastoreRepositories
import com.google.cloud.spring.data.datastore.repository.config.EnableDatastoreAuditing
import org.springframework.context.annotation.Configuration

@Configuration
@EnableDatastoreAuditing
class DatastoreConfig {

}