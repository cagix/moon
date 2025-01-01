(ns ^:no-doc anvil.entity.state.player-dead
  (:require [anvil.entity :as entity]
            [cdq.context :refer [show-modal]]
            [clojure.gdx :refer [play]]
            [clojure.component :refer [defcomponent]]
            [gdl.context :as c]))

(defcomponent :player-dead
  (entity/->v [[k] c]
    (c/build c :player-dead/component.enter))

  (entity/enter [[_ {:keys [tx/sound
                               modal/title
                               modal/text
                               modal/button-text]}]
                    c]
    (play sound)
    (show-modal c {:title title
                   :text text
                   :button-text button-text
                   :on-click (fn [])})))
