(ns ^:no-doc anvil.entity.state.player-dead
  (:require [anvil.component :as component]
            [clojure.gdx.audio.sound :as sound]
            [gdl.context.db :as db]
            [gdl.stage :refer [show-modal]]))

(defmethods :player-dead
  (component/->v [[k]]
    (db/build :player-dead/component.enter))

  (component/enter [[_ {:keys [tx/sound
                               modal/title
                               modal/text
                               modal/button-text]}]
                    c]
    (sound/play sound)
    (show-modal {:title title
                 :text text
                 :button-text button-text
                 :on-click (fn [])})))
