(ns cdq.ui.editor.widget.default
  (:require [cdq.ui.editor.widget :as widget]
            [cdq.utils :refer [truncate
                               ->edn-str]]
            [gdl.ui :as ui])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defmethod widget/create :default [_ v]
  (ui/label (truncate (->edn-str v) 60)))

(defmethod widget/value :default [_ widget]
  ((Actor/.getUserObject widget) 1))
