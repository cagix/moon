(ns cdq.ui.editor.schema.string
  (:require [cdq.ui.tooltip :as tooltip]
            [clojure.vis-ui.text-field :as text-field]))

(defn create [schema v _ctx]
  (tooltip/add! (text-field/create (str v))
                (str schema)))

(defn value [_ widget _schemas]
  (text-field/text widget))
