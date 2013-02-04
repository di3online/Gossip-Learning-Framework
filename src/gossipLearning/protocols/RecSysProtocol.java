package gossipLearning.protocols;

import gossipLearning.evaluators.RecSysResultAggregator;
import gossipLearning.interfaces.ModelHolder;
import gossipLearning.interfaces.models.Mergeable;
import gossipLearning.interfaces.models.Model;
import gossipLearning.messages.ModelMessage;
import gossipLearning.models.recsys.MergeableRecSysModel;
import gossipLearning.models.recsys.RecSysModel;
import gossipLearning.utils.BQModelHolder;
import gossipLearning.utils.InstanceHolder;
import gossipLearning.utils.SparseVector;
import peersim.core.CommonState;

public class RecSysProtocol extends LearningProtocol {

  private SparseVector userModel;
  
  public RecSysProtocol(String prefix) {
    super(prefix);
  }
  
  protected RecSysProtocol(RecSysProtocol a) {
    super(a);
  }
  
  @Override
  public Object clone() {
    return new RecSysProtocol(this);
  }
  
  public void init(String prefix) {
    super.init(prefix);
    resultAggregator = new RecSysResultAggregator(modelNames, evalNames);
    latestModelHolder = new BQModelHolder(modelNames.length);
  }
  
  protected ModelHolder latestModelHolder;
  @Override
  public void activeThread() {
    // evaluate
    for (int i = 0; i < modelHolders.length; i++) {
      if (CommonState.r.nextDouble() < evaluationProbability) {
        ((RecSysResultAggregator)resultAggregator).push(currentProtocolID, i, (int)currentNode.getID(), userModel, modelHolders[i], ((ExtractionProtocol)currentNode.getProtocol(exrtactorProtocolID)).getModel());
      }
    }
    
    // send
    if (numberOfIncomingModels == 0) {
      numberOfWaits ++;
    }
    if (numberOfWaits == numOfWaitingPeriods) {
      numberOfIncomingModels = 1;
      numberOfWaits = 0;
    }
    
    for (int id = Math.min(numberOfIncomingModels, capacity); id > 0; id --) {
      latestModelHolder.clear();
      for (int i = 0; i < modelHolders.length; i++) {  
        // store the latest models in a new modelHolder
        Model latestModel = modelHolders[i].getModel(modelHolders[i].size() - id);
        latestModelHolder.add(latestModel);
      }
      if (latestModelHolder.size() == modelHolders.length) {
        // send the latest models to a random neighbor
        sendToRandomNeighbor(new ModelMessage(currentNode, latestModelHolder, currentProtocolID));
      }
    }
    numberOfIncomingModels = 0;
  }
  
  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected void updateModels(ModelHolder modelHolder){
    // get instances from the extraction protocol
    InstanceHolder instances = ((ExtractionProtocol)currentNode.getProtocol(exrtactorProtocolID)).getInstances();
    for (int i = 0; i < modelHolder.size(); i++){
      // get the ith model from the modelHolder
      RecSysModel model = (RecSysModel)modelHolder.getModel(i);
      // if it is a mergeable model, them merge them
      if (model instanceof MergeableRecSysModel){
        RecSysModel lastSeen = (RecSysModel)lastSeenMergeableModels.getModel(i);
        lastSeenMergeableModels.setModel(i, (RecSysModel) model.clone());
        model = (RecSysModel)((Mergeable) model).merge(lastSeen);
      }
      // updating the model with the local training samples
      for (int sampleID = 0; instances != null && sampleID < instances.size(); sampleID ++) {
        // we use each samples for updating the currently processed model
        SparseVector x = instances.getInstance(sampleID);
        userModel = model.update(x, userModel);
      }
      // stores the updated model
      modelHolders[i].add(model);
    }
  }
  
  @Override
  public void setNumberOfClasses(int numberOfClasses) {
  }

}