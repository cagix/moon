(ns ^:no-doc anvil.entity.state.player-dead
  (:require [cdq.context :refer [show-modal]]
            [clojure.component :as component :refer [defcomponent]]
            [clojure.gdx :refer [play]]
            [gdl.context :as c]))

(defcomponent :player-dead
  (component/create [[k] c]
    (c/build c :player-dead/component.enter))

  (component/enter [[_ {:keys [tx/sound
                               modal/title
                               modal/text
                               modal/button-text]}]
                    c]
    (play sound)
    (show-modal c {:title title
                   :text text
                   :button-text button-text
                   :on-click (fn [])})))
