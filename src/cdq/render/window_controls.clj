(ns cdq.render.window-controls
  (:require [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.group :as group]))

(defn render [c]
  (let [window-hotkeys {:inventory-window   :i
                        :entity-info-window :e}]
    (doseq [window-id [:inventory-window
                       :entity-info-window]
            :when (input/key-just-pressed? (get window-hotkeys window-id))]
      (actor/toggle-visible! (get (:windows (:cdq.context/stage c)) window-id))))
  (when (input/key-just-pressed? :escape)
    (let [windows (group/children (:windows (:cdq.context/stage c)))]
      (when (some actor/visible? windows)
        (run! #(actor/set-visible % false) windows))))
  c)
