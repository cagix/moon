(ns cdq.ui.editor.widget.default
  (:require [clojure.utils :as utils]
            [clojure.vis-ui.label :as label])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn create [_ v _ctx]
  (label/create (utils/truncate (utils/->edn-str v) 60)))

(defn value [_  widget _schemas]
  ((Actor/.getUserObject widget) 1))
