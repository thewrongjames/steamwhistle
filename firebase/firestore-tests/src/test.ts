import {assertFails, assertSucceeds} from "@firebase/rules-unit-testing";
import {FirebaseError} from "@firebase/util";
import {DocumentReference, DocumentSnapshot} from "firebase/firestore";

export async function test(
    testName: string,
    operationPromise: Promise<void | DocumentReference | DocumentSnapshot>,
    expectsSuccess: boolean,
) {
  console.log(`RUNNING: ${testName}`);

  const asserter = expectsSuccess ? assertSucceeds : assertFails;
  const testNotPassedMessage = expectsSuccess ?
    "Expected success but got failure" :
    "Expected failure but got success";

  try {
    await asserter(operationPromise);
  } catch (error) {
    if (!(error instanceof FirebaseError)) {
      throw error;
    }

    console.error(error);
    console.log(`TEST NOT PASSED: ${testNotPassedMessage}\n`);
    return;
  }

  console.log("TEST PASSED\n");
}
