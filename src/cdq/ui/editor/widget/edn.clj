(ns cdq.ui.editor.widget.edn
  (:require [cdq.ui.tooltip :as tooltip]
            [clojure.edn :as edn]
            [clojure.utils :as utils]
            [clojure.vis-ui.text-field :as text-field]))

(defn create [schema v _ctx]
  (tooltip/add! (text-field/create (utils/->edn-str v))
                (str schema)))

(defn value [_  widget _schemas]
  (edn/read-string (text-field/text widget)))
