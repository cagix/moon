(ns clojure.app)

(def state (atom nil))

(defn post-runnable [f]
  (.postRunnable com.badlogic.gdx.Gdx/app #(f @state)))
