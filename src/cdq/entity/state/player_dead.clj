(ns cdq.entity.state.player-dead
  (:require [cdq.context :refer [show-modal]]
            [clojure.gdx :refer [play]]
            [gdl.context :as c]))

(defn cursor [_]
  :cursors/black-x)

(defn pause-game? [_]
  true)

(defn enter [[_ {:keys [tx/sound
                        modal/title
                        modal/text
                        modal/button-text]}]
             c]
  (play sound)
  (show-modal c {:title title
                 :text text
                 :button-text button-text
                 :on-click (fn [])}))
