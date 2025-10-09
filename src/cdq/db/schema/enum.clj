(ns cdq.db.schema.enum
  (:require [clojure.edn :as edn]
            [clojure.vis-ui.select-box :as select-box]
            [clojure.utils :as utils]))

(defn create [schema v _ctx]
  (select-box/create
   {:items (map utils/->edn-str (rest schema))
    :selected (utils/->edn-str v)}))

(defn value [_  widget _schemas]
  (edn/read-string (select-box/selected widget)))
