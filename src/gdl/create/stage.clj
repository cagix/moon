(ns gdl.create.stage
  (:require [clojure.gdx.input :as input]
            [gdl.ui :as ui]
            [gdl.ui.stage :as stage]))

(defn- reify-stage [stage]
  (reify
    ; TODO is disposable but not sure if needed as we handle batch ourself.
    clojure.lang.ILookup
    (valAt [_ key]
      (key stage))

    stage/Stage
    (render! [_ ctx]
      (ui/act! stage ctx)
      (ui/draw! stage ctx)
      ctx)

    (add! [_ actor] ; -> re-use gdl.ui/add! ?
      (ui/add! stage actor))

    (clear! [_]
      (ui/clear! stage))

    (hit [_ position]
      (ui/hit stage position))

    (find-actor [_ actor-name]
      (-> stage
          ui/root
          (ui/find-actor actor-name)))))

(defn create! [ui-config graphics input]
  (ui/load! ui-config)
  (let [stage (ui/stage (:java-object (:ui-viewport graphics))
                        (:batch graphics))]
    (input/set-processor! input stage)
    (reify-stage stage)))
