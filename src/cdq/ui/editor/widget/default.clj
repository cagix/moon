(ns cdq.ui.editor.widget.default
  (:require [clojure.utils :as utils]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.vis-ui.label :as label]))

(defn create [_ v _ctx]
  (label/create (utils/truncate (utils/->edn-str v) 60)))

(defn value [_  widget _schemas]
  ((actor/user-object widget) 1))
