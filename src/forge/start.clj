(ns forge.start)

; cannot have main in forge.core as it is 'clojure.core' !
(defn -main []
  (start-app "app.edn"))
