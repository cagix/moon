(ns cdq.editor.widget.enum
  (:require [cdq.utils :as utils]
            [clojure.edn :as edn]
            [clojure.vis-ui.select-box :as select-box]))

(defn create [schema _attribute v _ctx]
  {:actor/type :actor.type/select-box
   :items (map utils/->edn-str (rest schema))
   :selected (utils/->edn-str v)})

(defn value [_  _attribute widget _schemas]
  (edn/read-string (select-box/get-selected widget)))
