(ns cdq.ui.tooltip
  (:require [clojure.gdx.vis-ui.widget.tooltip :as tooltip])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.utils Align)
           (com.kotcrab.vis.ui.widget VisLabel)))

(defn add! [actor tooltip-text]
  (tooltip/create {:update-fn (fn [tooltip]
                                (when-not (string? tooltip-text)
                                  (let [actor (tooltip/target tooltip)
                                        ctx (when-let [stage (Actor/.getStage actor)]
                                              (.ctx stage))]
                                    (when ctx
                                      (tooltip/set-text! tooltip (tooltip-text ctx))))))
                   :target actor
                   :content (doto (VisLabel. ^CharSequence
                                             (str (if (string? tooltip-text)
                                                    tooltip-text
                                                    "")))
                              (.setAlignment Align/center))})
  actor)

(defn remove! [actor]
  (tooltip/remove! actor))
