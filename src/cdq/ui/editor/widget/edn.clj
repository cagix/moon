(ns cdq.ui.editor.widget.edn
  (:require [clojure.edn :as edn]
            [clojure.scene2d.vis-ui.text-field :as text-field]
            [clojure.utils :as utils]))

(defn create [schema v _ctx]
  (text-field/create
   {:text-field/text (utils/->edn-str v)
    :tooltip (str schema)}))

(defn value [_  widget _schemas]
  (edn/read-string (text-field/text widget)))
