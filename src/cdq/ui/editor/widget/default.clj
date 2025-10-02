(ns cdq.ui.editor.widget.default
  (:require [gdl.scene2d.actor :as actor]
            [gdl.utils :as utils]))

(defn create [_ v _ctx]
  {:actor/type :actor.type/label
   :label/text (utils/truncate (utils/->edn-str v) 60)})

(defn value [_  widget _schemas]
  ((actor/user-object widget) 1))
