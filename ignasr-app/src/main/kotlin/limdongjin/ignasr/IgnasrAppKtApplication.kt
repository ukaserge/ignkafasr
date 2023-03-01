package limdongjin.ignasr

import com.google.cloud.spring.data.datastore.repository.config.EnableDatastoreRepositories
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@EnableDatastoreRepositories(datastoreTemplateRef="datastoreTemplate")
@SpringBootApplication
class IgnasrAppKtApplication

fun main(args: Array<String>) {
	runApplication<IgnasrAppKtApplication>(*args)
}
