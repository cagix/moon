(ns cdq.tasks.create-assets-edn
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.io File)))

(defn list-files [folder extension]
  (let [url (io/resource folder)]
    (if (nil? url)
      (throw (ex-info "sounds/ directory not found on classpath" {}))
      (let [^File file (io/file url)]
        (->> (.listFiles file)
             (filter File/.isFile)
             (filter #(str/ends-with? (File/.getName %) extension))
             (map File/.getName))))))

(comment
 (list-files "sounds/" ".wav")

 )
