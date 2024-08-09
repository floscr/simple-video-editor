(ns app.bindings.dnd-kit.core
  (:require ["@dnd-kit/core" :as dnd-kit :refer [defaultDropAnimationSideEffects]]
            ["@dnd-kit/modifiers" :refer [snapCenterToCursor]]
            [goog.string :as gstring]
            [applied-science.js-interop :as j]
            [uix.core :refer [defui $]]))

(def pointer-within dnd-kit/pointerWithin)

;; (defn use-draggable
;;   ([id] (use-draggable id {}))
;;   ([id data]
;;    (-> (js->clj (dnd-kit/useDraggable #js {:id id
;;                                            :data data})
;;                 :keywordize-keys true)
;;        (#(doto % js/console.log))
;;        (update-in [:listeners] (fn [listeners] (js->clj listeners

(defn use-draggable
  ([id] (use-draggable id {}))
  ([id data]
   (let [{:keys [transform] :as m} (js->clj (dnd-kit/useDraggable #js {:id id
                                                                       :data data})
                                            :keywordize-keys true)]
     (cond-> m
         :always (update :listeners (fn [listeners] (js->clj listeners :keywordize-keys true)))
         transform (assoc :style {:transform (gstring/format "translate3D(%spx,%spx,0)" (:x transform) (:y transform))
                                  :z-index 9999
                                  :backdrop-filter "blur(10px)"})))))
:keywordize-keys true
(defn use-droppable
  ([id] (use-droppable id {}))
  ([id data]
   (js->clj (dnd-kit/useDroppable (clj->js {:id id
                                            :data data}))
            :keywordize-keys true)))

(def activation-constraint "Minimum pixels to move to count as a drag." 8)

(def drag-overlay dnd-kit/DragOverlay)

(def default-modifiers #js [snapCenterToCursor])

(def drop-animation
  ;; Fixes DragOver opacity being set to 0
  #js {"sideEffects"
       (defaultDropAnimationSideEffects
             (j/lit {:styles {:active {:opacity 1}}}))})

(defn use-default-sensors []
  (dnd-kit/useSensors
   (dnd-kit/useSensor dnd-kit/MouseSensor
                      (j/lit {:activationConstraint {:distance activation-constraint}}))
   (dnd-kit/useSensor dnd-kit/PointerSensor
                      (j/lit {:activationConstraint
                              {:distance 8}}))))

(defui context [{:keys [on-drag-start
                        on-drag-end
                        on-drag-move
                        collision-detection
                        children
                        modifiers
                        sensors]
                 :as _props
                 :or {modifiers default-modifiers
                      sensors (use-default-sensors)
                      collision-detection dnd-kit/pointerWithin}}]
  ($ dnd-kit/DndContext
     {:modifiers modifiers
      :sensors sensors
      :onDragStart on-drag-start
      :onDragEnd on-drag-end
      :onDragMove on-drag-move
      :collisionDetection collision-detection}
     children))
