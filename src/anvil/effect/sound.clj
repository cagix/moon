(ns ^:no-doc anvil.effect.sound
  (:require [gdl.effect.component :as component]
            [clojure.gdx :refer [play]]
            [clojure.utils :refer [defmethods]]))

(defmethods :effects/sound
  (component/applicable? [_ _ctx]
    true)

  (component/useful? [_ _ _c]
    false)

  (component/handle [[_ sound] _ctx c]
    (play sound)))
