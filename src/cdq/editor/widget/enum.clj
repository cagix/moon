(ns cdq.editor.widget.enum
  (:require [cdq.ui.select-box :as select-box]
            [cdq.utils :refer [->edn-str]]
            [clojure.edn :as edn]))

(defn create [schema _attribute v _ctx]
  {:actor/type :actor.type/select-box
   :items (map ->edn-str (rest schema))
   :selected (->edn-str v)})

(defn value [_  _attribute widget _schemas]
  (edn/read-string (select-box/get-selected widget)))

