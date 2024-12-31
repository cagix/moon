(ns ^:no-doc anvil.entity.state.player-dead
  (:require [anvil.component :as component]
            [cdq.context :refer [show-modal]]
            [clojure.gdx :refer [play]]
            [clojure.utils :refer [defmethods]]
            [gdl.context :as c]))

(defmethods :player-dead
  (component/->v [[k] c]
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
