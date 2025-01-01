(ns ^:no-doc anvil.effect.sound
  (:require [clojure.gdx :refer [play]]
            [clojure.component :as component :refer [defcomponent]]))

(defcomponent :effects/sound
  (component/applicable? [_ _ctx]
    true)

  (component/useful? [_ _ _c]
    false)

  (component/handle [[_ sound] _ctx c]
    (play sound)))
