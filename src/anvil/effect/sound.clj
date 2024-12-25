(ns ^:no-doc anvil.effect.sound
  (:require [anvil.component :as component]
            [clojure.gdx.audio.sound :as sound]))

(defmethods :effects/sound
  (component/applicable? [_ _ctx]
    true)

  (component/useful? [_ _]
    false)

  (component/handle [[_ sound] _ctx c]
    (sound/play sound)))
