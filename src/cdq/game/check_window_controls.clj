(ns cdq.game.check-window-controls
  (:require [cdq.ctx :as ctx]
            [cdq.impl.stage :refer [toggle-visible!]])
  (:import (com.badlogic.gdx Gdx Input$Keys)
           (com.badlogic.gdx.scenes.scene2d Actor Group)))

(defn do! []
  (let [stage ctx/stage
        window-hotkeys {:inventory-window   Input$Keys/I
                        :entity-info-window Input$Keys/E}]
    (doseq [window-id [:inventory-window
                       :entity-info-window]
            :when (.isKeyJustPressed Gdx/input (get window-hotkeys window-id))]
      (toggle-visible! (get (:windows stage) window-id)))
    (when (.isKeyJustPressed Gdx/input Input$Keys/ESCAPE)
      (let [windows (Group/.getChildren (:windows stage))]
        (when (some Actor/.isVisible windows)
          (run! #(Actor/.setVisible % false) windows))))))
