(ns cdq.ui.editor.widget.default
  (:require [clojure.utils :as utils])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn create [_ v _ctx]
  {:actor/type :actor.type/label
   :label/text (utils/truncate (utils/->edn-str v) 60)})

(defn value [_  widget _schemas]
  ((Actor/.getUserObject widget) 1))
