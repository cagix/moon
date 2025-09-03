(ns cdq.ui.editor.widget.enum
  (:require [cdq.utils :refer [->edn-str]]
            [clojure.edn :as edn]
            [cdq.gdx.ui :as ui]))

(defn create [schema _attribute v _ctx]
  {:actor/type :actor.type/select-box
   :items (map ->edn-str (rest schema))
   :selected (->edn-str v)})

(defn value [_  _attribute widget _schemas]
  (edn/read-string (ui/get-selected widget)))

