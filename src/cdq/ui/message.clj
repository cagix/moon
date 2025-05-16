(ns cdq.ui.message
  (:require [cdq.ctx :as ctx]
            [cdq.draw :as draw]
            [cdq.stage :as stage])
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)))

(defn create []
  (doto (proxy [Actor] []
          (draw [_batch _parent-alpha]
            (let [state (Actor/.getUserObject this)]
              (when-let [text (:text @state)]
                (draw/text {:x (/ (:width     ctx/ui-viewport) 2)
                            :y (+ (/ (:height ctx/ui-viewport) 2) 200)
                            :text text
                            :scale 2.5
                            :up? true}))))
          (act [delta]
            (let [state (Actor/.getUserObject this)]
              (when (:text @state)
                (swap! state update :counter + delta)
                (when (>= (:counter @state) 1.5)
                  (reset! state nil))))))
    (.setUserObject (atom nil))
    (.setName "player-message-actor")))

(defn show! [stage text]
  (Actor/.setUserObject (Group/.findActor (stage/root stage) "player-message-actor")
                        (atom {:text text
                               :counter 0})))
