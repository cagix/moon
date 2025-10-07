(ns cdq.ui.tooltip
  (:require [cdq.ui.stage :as stage]
            [clojure.gdx.utils.align :as align]
            [clojure.gdx.vis-ui.widget.tooltip :as tooltip])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.kotcrab.vis.ui.widget VisLabel)))

(defn add! [actor tooltip-text]
  (tooltip/create {:update-fn (fn [tooltip]
                                (when-not (string? tooltip-text)
                                  (let [actor (tooltip/target tooltip)
                                        ctx (when-let [stage (Actor/.getStage actor)]
                                              (stage/get-ctx stage))]
                                    (when ctx
                                      (tooltip/set-text! tooltip (tooltip-text ctx))))))
                   :target actor
                   :content (doto (VisLabel. ^CharSequence
                                             (str (if (string? tooltip-text)
                                                    tooltip-text
                                                    "")))
                              (.setAlignment (align/k->value :center)))})
  actor)

(defn remove! [actor]
  (tooltip/remove! actor))
