(ns forge.start)

(defn -main []
  (-> "app.edn"
      io-resource
      slurp
      edn-read-string
      start-app))
